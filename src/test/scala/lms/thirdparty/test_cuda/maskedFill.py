import os
import torch
import torch.nn as nn
import sys
from utils import extend, get_printer


def generate_data(lms_clean_root: str):
    torch.manual_seed(0)

    # model
    d0 = 64
    d1 = 64
    input = torch.randn(d0, d1)
    mask = torch.randint(0, 2, (d0, d1))

    output = input.masked_fill(mask, 0.0)

    # printer
    printer = get_printer(lms_clean_root, test_name = "maskedFill")
    printer("input.data", input)
    printer("mask.data", mask)
    printer("output.data", output)

if __name__ == '__main__':
    assert len(sys.argv) > 1, "must provide lms_clean_root dir"
    generate_data(sys.argv[1])