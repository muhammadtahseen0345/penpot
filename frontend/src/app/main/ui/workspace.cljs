;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; This Source Code Form is "Incompatible With Secondary Licenses", as
;; defined by the Mozilla Public License, v. 2.0.
;;
;; Copyright (c) 2020 UXBOX Labs SL

(ns app.main.ui.workspace
  (:require
   [app.common.geom.point :as gpt]
   [app.main.constants :as c]
   [app.main.data.history :as udh]
   [app.main.data.workspace :as dw]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.streams :as ms]
   [app.main.ui.hooks :as hooks]
   [app.main.ui.icons :as i]
   [app.main.ui.keyboard :as kbd]
   [app.main.ui.context :as ctx]
   [app.main.ui.workspace.colorpalette :refer [colorpalette]]
   [app.main.ui.workspace.colorpicker]
   [app.main.ui.workspace.context-menu :refer [context-menu]]
   [app.main.ui.workspace.header :refer [header]]
   [app.main.ui.workspace.left-toolbar :refer [left-toolbar]]
   [app.main.ui.workspace.libraries]
   [app.main.ui.workspace.rules :refer [horizontal-rule vertical-rule]]
   [app.main.ui.workspace.scroll :as scroll]
   [app.main.ui.workspace.sidebar :refer [left-sidebar right-sidebar]]
   [app.main.ui.workspace.viewport :refer [viewport coordinates]]
   [app.util.dom :as dom]
   [beicon.core :as rx]
   [cuerdas.core :as str]
   [okulary.core :as l]
   [rumext.alpha :as mf]))

;; --- Workspace

(mf/defc workspace-rules
  {::mf/wrap-props false}
  [props]
  (let [local (unchecked-get props "local")]
    [:*
     [:div.empty-rule-square]
     [:& horizontal-rule {:zoom (:zoom local)
                          :vbox (:vbox local)
                          :vport (:vport local)}]
     [:& vertical-rule {:zoom (:zoom local 1)
                        :vbox (:vbox local)
                        :vport (:vport local)}]
     [:& coordinates]]))

(mf/defc workspace-content
  [{:keys [file layout local] :as params}]
  [:*
   ;; TODO: left-sidebar option is obsolete because left-sidebar now
   ;; is always visible.
   (when (:colorpalette layout)
     [:& colorpalette {:left-sidebar? true}])

   [:section.workspace-content
    [:section.workspace-viewport
     (when (contains? layout :rules)
       [:& workspace-rules {:local local}])

     [:& viewport {:file file
                   :local local
                   :layout layout}]]]

   [:& left-toolbar {:layout layout}]

   ;; Aside
   [:& left-sidebar {:layout layout}]
   [:& right-sidebar {:local local}]])

(def trimmed-page-ref (l/derived :trimmed-page st/state =))

(mf/defc workspace-page
  [{:keys [file layout page-id] :as props}]
  (let [local (mf/deref refs/workspace-local)
        page  (mf/deref trimmed-page-ref)]
    (mf/use-layout-effect
     (mf/deps page-id)
     (fn []
       (st/emit! (dw/initialize-page page-id))
       (st/emitf (dw/finalize-page page-id))))

    (when page
      [:& workspace-content {:file file :layout layout :local local}])))

(mf/defc workspace-loader
  []
  [:div.workspace-loader
   i/loader-pencil])

(mf/defc workspace
  [{:keys [project-id file-id page-id] :as props}]
  (mf/use-effect #(st/emit! dw/initialize-layout))

  (mf/use-effect
   (mf/deps project-id file-id)
   (fn []
     (st/emit! (dw/initialize-file project-id file-id))
     (st/emitf (dw/finalize-file project-id file-id))))

  (hooks/use-shortcuts dw/shortcuts)

  (let [file    (mf/deref refs/workspace-file)
        project (mf/deref refs/workspace-project)
        layout  (mf/deref refs/workspace-layout)]

    [:& (mf/provider ctx/current-file-id) {:value (:id file)}
     [:& (mf/provider ctx/current-team-id) {:value (:team-id project)}
      [:& (mf/provider ctx/current-project-id) {:value (:id project)}
       [:& (mf/provider ctx/current-page-id) {:value page-id}

        [:section#workspace
         [:& header {:file file
                     :page-id page-id
                     :project project
                     :layout layout}]

         [:& context-menu]

         (if (and (and file project)
                  (:initialized file))
           [:& workspace-page {:page-id page-id :file file :layout layout}]
           [:& workspace-loader])]]]]]))

