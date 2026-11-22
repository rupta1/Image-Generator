#!/usr/bin/env python3
"""Experimental dynamic INT8 quantization helper for Diffusers SD1.5 ONNX UNet.

Requires: pip install onnx onnxruntime
Writes a new graph; never overwrites the input. Quantization can be slow and
may require substantial RAM/storage. It does not guarantee Android compatibility.
"""
import argparse
import os
import sys
from pathlib import Path

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--input", required=True, type=Path, help="UNet model.onnx")
    p.add_argument("--output", required=True, type=Path, help="Output quantized ONNX graph")
    p.add_argument("--weight-type", choices=["QInt8", "QUInt8"], default="QInt8")
    args = p.parse_args()
    if not args.input.is_file():
        p.error("input graph not found")
    if args.input.resolve() == args.output.resolve():
        p.error("input and output must be different files")
    try:
        from onnxruntime.quantization import quantize_dynamic, QuantType
    except ImportError:
        print("Install onnxruntime and onnx: pip install onnx onnxruntime", file=sys.stderr)
        return 2
    args.output.parent.mkdir(parents=True, exist_ok=True)
    qt = QuantType.QInt8 if args.weight_type == "QInt8" else QuantType.QUInt8
    print("Quantizing UNet weights (experimental)...", flush=True)
    try:
        quantize_dynamic(str(args.input), str(args.output), weight_type=qt)
    except Exception as exc:
        print(f"Quantization failed: {exc}", file=sys.stderr)
        return 1
    print(f"Output: {args.output} ({args.output.stat().st_size:,} bytes)")
    print("This is a graph-level experiment. Run Android pack validation and device tests before use.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
