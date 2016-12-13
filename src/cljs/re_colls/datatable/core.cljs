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
        (assoc-in (columns-def-db-path db-id)
                  columns-def)
        (assoc-in (options-db-path db-id)
                  options)
        (assoc-in (state-db-path db-id)
                  (merge-with merge
                              {:pagination {:per-page 10
                                            :cur-page 0}}
                              (select-keys options [:pagination]))))))


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


(re-frame/reg-event-db
  ::change-state-value
  [trim-v]
  (fn [db [db-id state-path new-val]]
    (assoc-in db (vec (concat (state-db-path db-id) state-path)) new-val)))


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
    (let [sort-data (fn [coll]
                      (let [{:keys [sort-key sort-comp]} (:sort state)]
                        (if sort-key
                          (sort-by #(get-in % sort-key) sort-comp coll)
                          coll)))

          paginate-data (fn [coll]
                          (let [{:keys [cur-page per-page] :as pagination} (:pagination state)]
                            (if (:enabled? pagination)
                              (->> coll
                                   (drop (* cur-page per-page))
                                   (take per-page))
                              coll)))]

      {:items (->> items
                   (sort-data)
                   (paginate-data))
       :state (-> state
                  (assoc-in [:pagination :total-pages]
                            (Math/ceil (/ (count items) (get-in state [:pagination :per-page])))))})))




; --- Views ---

(defn page-selector [db-id pagination]
  (let [{:keys [total-pages cur-page]} pagination]
    [:div.page-selector
     (let [prev-enabled? (not= cur-page 0)]
       [:span
        (merge
          {:on-click #(when prev-enabled?
                       (re-frame/dispatch [::change-state-value
                                           db-id
                                           [:pagination :cur-page]
                                           (dec cur-page)]))
           :style    {:cursor "pointer"}}
          (when-not prev-enabled?
            {:disabled "disabled"}))

        (str \u25C4 " PREVIOUS ")])


     [:select
      {:value     cur-page
       :on-change #(re-frame/dispatch [::change-state-value
                                       db-id
                                       [:pagination :cur-page]
                                       (js/parseInt (-> % .-target .-value))])}
      (doall
        (for [page-index (range total-pages)]
          ^{:key page-index}
          [:option
           {:value page-index}
           (str "Page " (inc page-index) " of " total-pages)]))]

     (let [next-enabled? (not= cur-page (dec total-pages))]
       [:span
        (merge
          {:on-click #(when next-enabled?
                       (re-frame/dispatch [::change-state-value
                                           db-id
                                           [:pagination :cur-page]
                                           (inc cur-page)]))
           :style    {:cursor "pointer"}}
          (when-not next-enabled?
            {:disabled "disabled"}))
        (str " NEXT " \u25BA)])]))



(defn datatable [db-id data-sub columns-def & [options]]
  (let [view-data (re-frame/subscribe [::data db-id data-sub])]
    (reagent/create-class
      {:component-will-mount
       #(re-frame/dispatch [::init db-id columns-def options])


       :component-function
       (fn [db-id data-sub columns-def & [options]]
         (let [{:keys [items state]} @view-data]
           [:div.re-colls-datatable
            (when (get-in state [:pagination :enabled?])
              [page-selector db-id (:pagination state)])

            [:table
             [:thead
              [:tr
               (doall
                 (for [{:keys [key label sorting]} columns-def]
                   ^{:key (str key)}
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
                       ^{:key (str key)}
                       [:td
                        (if render-fn
                          [render-fn (get-in data-entry key)]
                          (get-in data-entry key))]))]))]]]))})))
