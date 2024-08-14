#!/bin/bash

time (
    seq 500 | 
    sed 'c sudoku_2.txt' | 
    xargs cat | 
    java -cp bin Sudoku > /dev/null
)