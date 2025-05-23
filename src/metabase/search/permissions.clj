(ns metabase.search.permissions
  (:require
   [metabase.app-db.core :as mdb]
   [metabase.collections.models.collection :as collection]
   [metabase.models.interface :as mi]
   [metabase.permissions.core :as perms]
   [metabase.search.config :refer [SearchContext]]
   [metabase.util.malli :as mu]))

(defn- assert-current-user! [missing-param]
  (assert @@(requiring-resolve 'metabase.api.common/*current-user*)
          (format "metabase.api.common/*current-user* must be bound if %s is missing from search-ctx" missing-param)))

(defn- impersonated-user? [{:keys [is-impersonated-user?] :as _search-ctx}]
  (or is-impersonated-user?
      ;; TODO Make this parameter non-optional, and fix code paths that omit it. Then remove this fallback.
      (when (nil? is-impersonated-user?)
        (assert-current-user! :is-impersonated-user?)
        (perms/impersonated-user?))))

(defn- sandboxed-user? [{:keys [is-sandboxed-user?] :as _search-ctx}]
  (or is-sandboxed-user?
      ;; TODO Make this parameter non-optional, and fix code paths that omit it. Then remove this fallback.
      (when (nil? is-sandboxed-user?)
        (assert-current-user! :is-sandboxed-user?)
        (perms/sandboxed-user?))))

(defn sandboxed-or-impersonated-user?
  "Is the current user sandboxed or impersonated?"
  [search-ctx]
  (or (impersonated-user? search-ctx) (sandboxed-user? search-ctx)))

(mu/defn permitted-collections-clause
  "Build the WHERE clause corresponding to which collections the given user has access to."
  [{:keys [archived current-user-id is-superuser?]} :- SearchContext collection-id-col :- :keyword]
  [:and
   (collection/visible-collection-filter-clause
    collection-id-col
    {:include-archived-items    :all
     :include-trash-collection? true
     :permission-level          (if archived :write :read)}
    {:current-user-id current-user-id
     :is-superuser?   is-superuser?})
   (perms/audit-namespace-clause :collection.namespace nil)])

(mu/defn permitted-tables-clause
  "Build the WHERE clause corresponding to which tables the given user has access to."
  [{:keys [current-user-id is-superuser?]} :- SearchContext table-id-col :- :keyword]
  (mi/visible-filter-clause
   :model/Table
   [:cast table-id-col (case (mdb/db-type)
                         :mysql :signed
                         :integer)]
   {:user-id current-user-id
    :is-superuser? is-superuser?}
   {:perms/view-data :unrestricted
    :perms/create-queries :query-builder}))
