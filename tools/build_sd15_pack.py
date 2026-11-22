#!/usr/bin/env python3
"""Build a flat, verified Image Generator ONNX pack from a Diffusers ONNX export.

Requires: pip install onnx
Usage:
 python build_sd15_pack.py --root /path/to/stable-diffusion-v1-5-ONNX --out sd15-pack.zip
"""
import argparse, hashlib, json, shutil, zipfile
from pathlib import Path

def sha256(path):
    h=hashlib.sha256()
    with path.open("rb") as f:
        for b in iter(lambda:f.read(1024*1024), b""): h.update(b)
    return h.hexdigest()

def tensor_spec(value):
    tensor=value.type.tensor_type
    dims=[]
    for d in tensor.shape.dim:
        dims.append(int(d.dim_value) if d.dim_value else -1)
    typ={1:"float32",10:"float16",6:"int32",7:"int64"}.get(tensor.elem_type)
    if typ is None: raise ValueError(f"Unsupported ONNX tensor dtype {tensor.elem_type} for {value.name}")
    return {"name":value.name,"dimensions":dims,"dataType":typ}

def external_files(model):
    found=set()
    for node in model.graph.node:
        for attr in node.attribute:
            if attr.type == attr.GRAPH:
                graphs=[attr.g]
            elif attr.type == attr.GRAPHS:
                graphs=list(attr.graphs)
            else:
                graphs=[]
            for g in graphs:
                for n in g.node:
                    for a in n.attribute:
                        if a.type == a.GRAPH:
                            graphs.append(a.g)
        # initializer metadata
    for init in model.graph.initializer:
        if init.data_location == 1:
            for item in init.external_data:
                if item.key=="location": found.add(item.value)
    return found

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--root",required=True,type=Path)
    ap.add_argument("--out",required=True,type=Path)
    args=ap.parse_args()
    root=args.root.resolve()
    import onnx
    locations={"text_encoder":root/"text_encoder"/"model.onnx",
               "unet":root/"unet"/"model.onnx",
               "vae_decoder":root/"vae_decoder"/"model.onnx"}
    pack={}
    for k,p in locations.items():
        if not p.is_file(): raise SystemExit(f"Missing graph: {p}")
        pack[k]=onnx.load(str(p),load_external_data=False)
    outdir=args.out.with_suffix(".pack")
    if outdir.exists(): shutil.rmtree(outdir)
    outdir.mkdir(parents=True)
    graph_files={"text_encoder":"text_encoder.onnx","unet":"unet.onnx","vae_decoder":"vae_decoder.onnx"}
    for key,model in pack.items():
        shutil.copy2(locations[key],outdir/graph_files[key])
        # Preserve external data references beside the graph (flat pack layout).
        for name in external_files(model):
            src=(locations[key].parent/name).resolve()
            if not src.is_file(): raise SystemExit(f"Missing external tensor file: {src}")
            dest=outdir/name
            if dest.exists() and sha256(dest)!=sha256(src): raise SystemExit(f"External-data filename collision: {name}")
            if not dest.exists(): shutil.copy2(src,dest)
    tok=root/"tokenizer"
    for name in ("vocab.json","merges.txt","tokenizer_config.json","special_tokens_map.json"):
        src=tok/name
        if src.is_file(): shutil.copy2(src,outdir/name)
    if not (outdir/"vocab.json").is_file() or not (outdir/"merges.txt").is_file():
        raise SystemExit("Tokenizer vocab.json and merges.txt are required.")
    # Convert PNDM metadata to DDIM metadata with identical beta parameters.
    cfg=json.loads((root/"scheduler"/"scheduler_config.json").read_text())
    cfg["_class_name"]="DDIMScheduler"
    cfg["scheduler_type"]="DDIMScheduler"
    cfg["beta_schedule"]=cfg.get("beta_schedule","scaled_linear")
    cfg["prediction_type"]=cfg.get("prediction_type","epsilon")
    cfg.pop("trained_betas",None)
    (outdir/"scheduler_config.json").write_text(json.dumps(cfg,indent=2))
    interfaces={}
    for key,model in pack.items():
        inputs=[tensor_spec(v) for v in model.graph.input if v.name not in {x.name for x in model.graph.initializer}]
        outputs=[tensor_spec(v) for v in model.graph.output]
        interfaces[key]={"inputs":inputs,"outputs":outputs}
    contract={"formatVersion":1,"modelFamily":"sd15","latentChannels":4,"vaeScale":8,
              "maxTokenLength":77,"tokenizerFiles":{"vocab":"vocab.json","mergesOrConfig":"merges.txt"},
              "schedulerConfig":"scheduler_config.json","graphInterfaces":interfaces}
    (outdir/"pipeline-contract.json").write_text(json.dumps(contract,indent=2))
    manifest={"formatVersion":1,"displayName":"Stable Diffusion 1.5 ONNX","graphs":graph_files,
              "sha256":{k:sha256(outdir/v) for k,v in graph_files.items()}}
    (outdir/"model-manifest.json").write_text(json.dumps(manifest,indent=2))
    args.out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(args.out,"w",compression=zipfile.ZIP_STORED) as z:
        for p in sorted(outdir.iterdir()):
            if p.is_file(): z.write(p,p.name)
    print(f"Pack created: {args.out} ({args.out.stat().st_size} bytes)")
    print("Review model pack and licence before importing; external-data files are included.")

if __name__=="__main__": main()
