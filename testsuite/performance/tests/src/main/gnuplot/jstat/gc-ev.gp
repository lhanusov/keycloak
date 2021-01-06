set title "Utilisation of Garbage collection time (young, full, total)"
plot for [i in "YGC FGC"] datafile using 1:i title columnheader(i) with lines
