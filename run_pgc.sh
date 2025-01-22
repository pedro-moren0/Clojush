#!/bin/bash

bms=(
"checksum"
"digits"
"even-squares"
"for-loop-index"
"grade"
"pig-latin"
"replace-space-with-newline"
# "scrabble-score"
"string-differences"
"string-lengths-backwards"
"syllables"
"word-stats"
)

if [ $# != 1 ]; then
	echo "Invalid number of arguments!"
	echo "Usage: ./run_pgc.sh [WORK_DIR]"
else
	work_dir=$1
	output_folder="$work_dir/data"
	mkdir -p "$output_folder"

	for b in ${bms[@]};
	do
		for i in {1..100};
		do
			# pula os benchmarks já rodados
			if [ $b == "checksum" ] && [ $i -le 15 ]; then
				continue
			fi
			echo "Running $b $i/100"
			out_file="$b-$i.out"
			lein with-profile beluga run clojush.problems.software."$b" > "$output_folder/$out_file"
		done
	done
fi
