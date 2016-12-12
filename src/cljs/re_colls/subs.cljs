(ns re-colls.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))


(re-frame/reg-sub
  ::songs-list
  (fn [db]
    (get-in db [:sample-data :songs])))
