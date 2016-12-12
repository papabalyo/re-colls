(ns re-colls.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [re-colls.core-test]))

(doo-tests 're-colls.core-test)
