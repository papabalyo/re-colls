(ns re-frame-datatable.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [trim-v]]
            [cljs.spec :as s]))


; --- Model (spec) ---

(s/def ::db-id keyword?)
(s/def ::enabled? boolean?)
(s/def ::css-classes (s/coll-of string?))


; columns-def

(s/def ::column-key (s/coll-of keyword? :kind vector :min-count 1))
(s/def ::column-label string?)
(s/def ::sorting (s/keys :req [::enabled?]))
(s/def ::th-classes ::css-classes)


(s/def ::column-def
  (s/keys :req [::column-key ::column-label]
          :opt [::sorting ::th-classes]))

(s/def ::columns-def (s/coll-of ::column-def :min-count 1))


; options

(s/def ::table-classes ::css-classes)

(s/def ::per-page (s/and integer? pos?))
(s/def ::cur-page (s/and integer? (complement neg?)))
(s/def ::total-pages (s/and integer? pos?))
(s/def ::pagination
  (s/keys :req [::enabled?]
          :opt [::per-page ::cur-page ::total-pages]))


(s/def ::options
  (s/nilable
    (s/keys :opt [::pagination ::table-classes])))


; --- Re-frame database paths ---

(def root-db-path [:re-colls :datatable])
(defn db-path-for [db-path db-id]
  (vec (concat (conj root-db-path db-id)
               db-path)))

(def columns-def-db-path (partial db-path-for [:columns-def]))
(def options-db-path (partial db-path-for [:options]))
(def state-db-path (partial db-path-for [:state]))
(def sort-key-db-path (partial db-path-for [:state :sort :sort-key]))
(def sort-comp-db-path (partial db-path-for [:state :sort :sort-comp]))


; --- Defaults ---

(def per-page 10)



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
                  {::pagination (merge {::per-page per-page
                                        ::cur-page 0}
                                       (select-keys (::pagination options) [::per-page ::enabled?]))}))))


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
                          (let [{:keys [::cur-page ::per-page ::enabled?] :as pagination} (::pagination state)]
                            (if enabled?
                              (->> coll
                                   (drop (* cur-page per-page))
                                   (take per-page))
                              coll)))]

      {:items (->> items
                   (sort-data)
                   (paginate-data))
       :state (-> state
                  (assoc-in [::pagination ::total-pages]
                            (Math/ceil (/ (count items) (get-in state [::pagination ::per-page])))))})))




; --- Views ---

(defn page-selector [db-id pagination]
  (let [{:keys [::total-pages ::cur-page]} pagination]
    [:div.page-selector
     {:style {:float         "right"
              :margin-bottom "1em"}}
     (let [prev-enabled? (not= cur-page 0)]
       [:span
        (merge
          {:on-click #(when prev-enabled?
                        (re-frame/dispatch [::change-state-value
                                            db-id
                                            [::pagination ::cur-page]
                                            (dec cur-page)]))
           :style    {:cursor "pointer"}}
          (when-not prev-enabled?
            {:disabled "disabled"}))
        (str \u25C4 " PREVIOUS ")])


     [:select
      {:value     cur-page
       :on-change #(re-frame/dispatch [::change-state-value
                                       db-id
                                       [::pagination ::cur-page]
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
                                            [::pagination ::cur-page]
                                            (inc cur-page)]))
           :style    {:cursor "pointer"}}
          (when-not next-enabled?
            {:disabled "disabled"}))
        (str " NEXT " \u25BA)])]))


(defn datatable [db-id data-sub columns-def & [options]]
  {:pre [(or (s/valid? ::db-id db-id)
             (js/console.error (s/explain-str ::db-id db-id)))

         (or (s/valid? ::columns-def columns-def)
             (js/console.error (s/explain-str ::columns-def columns-def)))

         (or (s/valid? ::options options)
             (js/console.error (s/explain-str ::options options)))]}


  (let [view-data (re-frame/subscribe [::data db-id data-sub])]
    (reagent/create-class
      {:component-will-mount
       #(re-frame/dispatch [::init db-id columns-def options])


       :component-function
       (fn [db-id data-sub columns-def & [options]]
         (let [{:keys [items state]} @view-data]
           [:div.re-colls-datatable
            (when (get-in state [::pagination ::enabled?])
              [page-selector db-id (::pagination state)])

            [:table
             (when (::table-classes options)
               {:class (clojure.string/join \space (::table-classes options))})
             #_(merge
                 {}
                 (when (::table-classes options)
                   {:class (clojure.string/join \space (::table-classes options))}))

             [:thead
              [:tr
               (doall
                 (for [{:keys [::column-key ::column-label ::sorting ::th-classes]} columns-def]
                   ^{:key (str column-key)}
                   [:th
                    (merge
                      (when th-classes
                        {:class (clojure.string/join \space th-classes)
                         :style {:cursor "pointer"}})
                      {:on-click #(when (::enabled? sorting)
                                    (re-frame/dispatch [::set-sort-key db-id column-key]))})
                    column-label]))]]

             [:tbody
              (doall
                (for [[i data-entry] (map-indexed vector items)]
                  ^{:key i}
                  [:tr
                   (doall
                     (for [{:keys [::column-key render-fn]} columns-def]
                       ^{:key (str column-key)}
                       [:td
                        (if render-fn
                          [render-fn (get-in data-entry column-key)]
                          (get-in data-entry column-key))]))]))]]]))})))
