## Inflation and income (Spanish)

This code takes advantage of INEGI's database of prices (newly available afaik, before with PROFECO) 
to compute _ab initio_ food inflation indices of a basic food basket for selected cities.

The exercise grew out of my dissatisfaction with the official food inflation figures, often at variance 
from my own experience.

To make the exercise worthwhile, I also computed a basic basket that is a) in line with international
dietary guidelines (otherwise "basic" doesn't mean much), and b) roughly along the lines a of Mexican diet.
For there, we derived the price of a basket that can be used soundly to establish a basic line we
can use for further computations. 

(CONEVAL's method of computing the basic basket is rather obscure and does not, as far as I understand,
take the nourishment side of the equation into account).

The results are in the report. Susprisingly enough (we began from a different starting point), our
results are not too different from the official ones, although our result for people under the poverty
line once the rent credit is deducted is somewhat higher.


## Code

Three self-contained Java programs with variations of the idea. Also, some [R code for generating pdfs of 
plots.](R_code_INPC2.txt) In all cases the paths need to be revised both for input and output files / directories.


## Data

INEGI's database. Unfortunately, they don't allow a massive download of all the data so we had to do it
piecewise and ended up with a lot of them. [The zipped version of the entire set.](CSV)


## Status

Report is a final draft.

Calculations are finished, but there's plenty more to do. An obvious integration would be to integrate 
ENIGH data to have a more precise estimation of populations under the poverty line with more detail.
Another would be to extend the time series and include more / other cities.

If INEGI ever makes the data available through an API, we could once and for all build a page to
automatically compute this every month directly from official figures.
