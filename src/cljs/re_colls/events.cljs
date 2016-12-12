(ns re-colls.events
  (:require [re-frame.core :as re-frame]
            [re-colls.db :as db]))



(re-frame/reg-event-db
  ::initialize-db
  (fn []
    db/default-db))
