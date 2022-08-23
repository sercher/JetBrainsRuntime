#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcutil -h 10 0 250 11
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00   0.00  57.14  47.16  97.09  92.27      5     0.283    51    23.751     2     0.068    24.102
#
# -J-XX:+UseParallelGC -gcutil -h 10 0 250 11
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00 100.00  46.57  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  46.57  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  48.53  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  48.53  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  50.49  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  50.49  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  52.45  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  52.45  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  54.41  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  0.00 100.00  54.41  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00 100.00  56.37  14.56  94.70  89.25      5     0.206     0     0.000     -         -     0.206

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	    datalines2=0;
        }

/^  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*([0-9]+|-)[ ]*([0-9]+\.[0-9]+|-)[ ]*[0-9]+\.[0-9]+$/	{
	    if (headerlines == 2) {
	        datalines2++;
	    }
	    datalines++;
	}

	{ totallines++; print $0 }

END	{ 
	    if ((headerlines == 2) && (datalines == 11) && (datalines2 == 1)) {
	        exit 0
	    } else {
	        exit 1
	    }
	}
