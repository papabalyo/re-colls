(ns re-colls.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-colls.events :as events]
            [re-colls.subs]
            [re-colls.views :as views]
            [re-colls.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))


(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))


(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
