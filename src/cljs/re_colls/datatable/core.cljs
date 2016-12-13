(ns re-colls.datatable.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [debug trim-v]]))


(def root-db-path [:re-colls :datatable])
(defn db-path-for [db-path db-id]
  (vec (concat (conj root-db-path db-id)
               db-path)))

(def columns-def-db-path (partial db-path-for [:columns-def]))
(def options-db-path (partial db-path-for [:options]))
(def state-db-path (partial db-path-for [:state]))
(def sort-key-db-path (partial db-path-for [:state :sort :sort-key]))
(def sort-comp-db-path (partial db-path-for [:state :sort :sort-comp]))



; --- Events ---

(re-frame/reg-event-db
  ::init
  [trim-v]
  (fn [db [db-id columns-def options]]
    (-> db
        (assoc-in (columns-def-db-path db-id) columns-def)
        (assoc-in (options-db-path db-id) options))))


(re-frame/reg-event-db
  ::set-sort-key
  [trim-v]
  (fn [db [db-id sort-key]]
    (let [cur-sort-key (get-in db (sort-key-db-path db-id))
          cur-sort-comp (get-in db (sort-comp-db-path db-id) >)]
      (if (= cur-sort-key sort-key)
        (assoc-in db (sort-comp-db-path db-id)
                  (get {> < < >} cur-sort-comp))
        (-> db
            (assoc-in (sort-key-db-path db-id) sort-key)
            (assoc-in (sort-comp-db-path db-id) cur-sort-comp))))))


; --- Subs ---

(re-frame/reg-sub
  ::state
  (fn [db [_ db-id]]
    (get-in db (state-db-path db-id))))



(re-frame/reg-sub
  ::data
  (fn [[_ db-id data-sub]]
    [(re-frame/subscribe data-sub)
     (re-frame/subscribe [::state db-id])])

  (fn [[items state]]
    (let [{:keys [sort-key sort-comp]} (:sort state)]
      {:items (if sort-key
                (sort-by sort-key sort-comp items)
                items)
       :state state})))



; --- Views ---

(defn datatable [db-id data-sub columns-def & [options]]
  (let [view-data (re-frame/subscribe [::data db-id data-sub])]
    (reagent/create-class
      {:component-will-mount
       #(re-frame/dispatch [::init db-id columns-def options])


       :component-function
       (fn [db-id data-sub columns-def & [options]]
         (let [{:keys [items state]} @view-data]
           [:table
            [:thead
             [:tr
              (doall
                (for [{:keys [key label sorting]} columns-def]
                  ^{:key key}
                  [:th
                   {:style    {:cursor "pointer"}
                    :on-click #(when (:enabled? sorting)
                                 (re-frame/dispatch [::set-sort-key db-id key]))}
                   label]))]]

            [:tbody
             (doall
               (for [[i data-entry] (map-indexed vector items)]
                 ^{:key i}
                 [:tr
                  (doall
                    (for [{:keys [key render-fn]} columns-def]
                      ^{:key key}
                      [:td
                       (if render-fn
                         [render-fn (get data-entry key)]
                         (get data-entry key))]))]))]]))})))
