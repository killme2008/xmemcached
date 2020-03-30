# Benchmarks

## How to run

Just run `./run.sh` in this folder. 
It will take care of everything.

## Add new test

1. Create a separate Maven module with your library. 
    1. I guess it is best to copy-paste an existing case.
    2. Modify `logging.properties` to log to new file!

2. Add your execution to `./run.sh` for future comparisons.

3. Add your result file in `charts.js` so the HTML report 
 could pick up and render your results properly.
