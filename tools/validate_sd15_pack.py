#!/usr/bin/env python3
"""Lightweight audit of a generated SD15 pack ZIP; does not load ONNX tensor payloads."""
import argparse, hashlib, json, zipfile
from pathlib import PurePosixPath

def digest(zf, name):
    h=hashlib.sha256()
    with zf.open(name) as stream:
        for block in iter(lambda:stream.read(1024*1024), b""): h.update(block)
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser()
    p.add_argument("zip")
    args=p.parse_args()
    required={"model-manifest.json","pipeline-contract.json","text_encoder.onnx","unet.onnx","vae_decoder.onnx","vocab.json","merges.txt","scheduler_config.json"}
    with zipfile.ZipFile(args.zip) as z:
        names=set(z.namelist())
        bad=[n for n in names if PurePosixPath(n).name!=n or n.startswith("/") or ".." in PurePosixPath(n).parts]
        if bad: raise SystemExit("Unsafe/non-flat ZIP entries: "+", ".join(bad))
        missing=required-names
        if missing: raise SystemExit("Missing required files: "+", ".join(sorted(missing)))
        manifest=json.loads(z.read("model-manifest.json"))
        contract=json.loads(z.read("pipeline-contract.json"))
        if set(manifest.get("graphs",{})) != {"text_encoder","unet","vae_decoder"}:
            raise SystemExit("Manifest graph keys do not exactly match supported graph set.")
        if set(contract.get("graphInterfaces",{})) != {"text_encoder","unet","vae_decoder"}:
            raise SystemExit("Contract graph keys do not exactly match supported graph set.")
        for key,graph in manifest["graphs"].items():
            if graph not in names: raise SystemExit(f"Graph missing: {graph}")
            expected=manifest.get("sha256",{}).get(key)
            if expected and digest(z,graph)!=expected: raise SystemExit(f"Graph hash mismatch: {graph}")
        # Verify external-data locations referenced by ONNX protobuf without loading tensor data.
        try:
            import onnx
        except ImportError:
            print("NOTE: install ONNX to verify external-data references.")
        else:
            for key,graph in manifest["graphs"].items():
                model=onnx.load_model_from_string(z.read(graph))
                for init in model.graph.initializer:
                    if init.data_location==onnx.TensorProto.EXTERNAL:
                        locs=[x.value for x in init.external_data if x.key=="location"]
                        if not locs: raise SystemExit(f"{key}: external tensor has no location metadata")
                        for loc in locs:
                            if loc not in names: raise SystemExit(f"{key}: external-data file missing: {loc}")
                            if PurePosixPath(loc).name!=loc: raise SystemExit(f"{key}: non-flat external-data path: {loc}")
        print("Pack ZIP layout and graph hashes: OK")
        print(f"Files: {len(names)}; ZIP size: {sum(i.file_size for i in z.infolist())} bytes")
        print("Audit does not validate runtime tensor compatibility or image generation.")

if __name__=="__main__": main()
