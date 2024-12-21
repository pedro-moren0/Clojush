#!/bin/bash

bms=(
"checksum"
"digits"
"even-squares"
"for-loop-index"
"grade"
# "pig-latin"
"replace-space-with-newline"
# "scrabble-score"
# "string-differences"
"string-lengths-backwards"
"syllables"
# "word-stats"
)

for b in ${bms[@]};
do
	for i in {1..100};
	do
		echo "Running $b $i/100"
		out_file="$b-$i.out"
		lein run clojush.problems.software."$b" > "data/$out_file"
	done
done

# lein run clojush.problems.software.syllables > syllables.out
