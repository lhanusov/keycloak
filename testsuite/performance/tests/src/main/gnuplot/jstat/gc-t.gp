set title "Utilisation of Garbage collection events (young, full)"
plot for [i in "YGCT FGCT GCT"] datafile using 1:i title columnheader(i) with lines
