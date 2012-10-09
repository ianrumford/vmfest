(ns vmfest.virtualbox.virtualbox
  (:require [clojure.tools.logging :as log]
            [vmfest.virtualbox.model :as model]
            [vmfest.virtualbox.conditions :as conditions]
            [vmfest.virtualbox.enums :as enums]
            vmfest.virtualbox.guest-os-type)
  (:import [org.virtualbox_4_2
            VirtualBoxManager
            IVirtualBox
            VBoxException]
           [vmfest.virtualbox.model
            Server
            Machine]))

(defn find-vb-m
  [vbox id-or-name]
  {:pre [(model/IVirtualBox? vbox)]}
  (try
    (log/tracef "find-vb-m: looking for machine '%s'" id-or-name)
    (let [vb-m (.findMachine vbox id-or-name)]
      (log/debugf "find-vb-m: found machine '%s': %s" id-or-name vb-m)
      vb-m)
    (catch Exception e
      (log/warnf
       "find-vb-m: Machine identified by '%s' not found." id-or-name))))

(defn find-medium
  [vbox id-or-location & [type]]
  {:pre [(model/IVirtualBox? vbox)
         (if type (#{:hard-disk :floppy :dvd} type) true)]}
  (if (and type (not (#{:hard-disk :floppy :dvd} type)))
    ;; todo: throw condition
    (log/warnf
     "find-medium: medium type %s not in #{:hard-disk :floppy :dvd}" type)
    (let [type-key (or type :hard-disk)
          type (enums/key-to-device-type type-key)
          access-mode-key :read-only
          access-mode (enums/key-to-access-mode access-mode-key)
          ]
      (try (.openMedium vbox id-or-location type access-mode false)
        (catch Exception e
             (log/warnf "find-medium: location %s not found type %s access mode " id-or-location type access-mode))))))

(defn open-medium
  [vbox location & [type access-mode force-new-uuid?]]
  {:pre [(model/IVirtualBox? vbox)
         (if type (#{:hard-disk :floppy :dvd} type) true)]}
  (let [type (enums/key-to-device-type (or type :hard-disk))
        access-mode (enums/key-to-access-mode (or access-mode :read-write))
        force-new-uuid? (or force-new-uuid? false)]
    (conditions/with-vbox-exception-translation
      {:VBOX_E_FILE_ERROR
       "Invalid medium storage file location or could not find the medium
at the specified location."
       :VBOX_E_IPRT_ERROR
       "Could not get medium storage format."
       :E_INVALIDARG
       "Invalid medium storage format."
       :VBOX_E_INVALID_OBJECT_STATE
       "Medium has already been added to a media registry."}
      (.openMedium vbox location type access-mode force-new-uuid?))))

(defn register-machine [vbox machine]
  {:pre [(model/IVirtualBox? vbox)
         (model/IMachine? machine)]}
  (conditions/with-vbox-exception-translation
    {:VBOX_E_OBJECT_NOT_FOUND "No matching virtual machine found"
     :VBOX_E_INVALID_OBJECT_STATE
     "Virtual machine was not created within this VirtualBox instance."}
    (.registerMachine vbox machine)))

(defn create-machine
  ([vbox name os-type-id]
     (create-machine vbox name os-type-id false))
  ([vbox name os-type-id overwrite]
     (create-machine vbox name os-type-id overwrite nil))
  ([vbox name os-type-id overwrite base-folder]
     {:pre [(model/IVirtualBox? vbox)]}
     (let [path (when base-folder
                  (log/infof "CRT MAC X1 BASE_FOLDEF %s" base-folder)
                  (let [machine-filename  (.composeMachineFilename vbox name "" ""  base-folder)]
                    (println "CRT MAC MACHINE FILENAME" machine-filename)
                    machine-filename
                    ))
           ]
       (conditions/with-vbox-exception-translation
         {:VBOX_E_OBJECT_NOT_FOUND "invalid os type ID."
          :VBOX_E_FILE_ERROR
          (str "Resulting settings file name is invalid or the settings"
               " file already exists or could not be created due to an"
               " I/O error.")
          :E_INVALIDARG "name is empty or null."}
         (log/infof
          "create-machine: Creating machine %s in %s, %s overwriting previous contents"
          name
          path
          (if overwrite "" "not"))
         (log/infof "CRT MAC X2 PATH %s NAME %s OSTypeID %s" path name os-type-id )         
         #_(.createMachine vbox path name os-type-id nil overwrite)
         (.createMachine vbox path name nil os-type-id  "forceOverite=1" )
         ))))

;;; DHCP

(defn find-dhcp-by-interface-name
  "Find a dhcp server by the interface name to which it is attached.

  NOTE: This is not very reliable, as the original function does not
  do what it says it does. This function finds the dhcp by the *dhcp*
  name instead of the interface name. The DHCP server is usually named
  with the pattern `HostInterfaceNetworking-vboxnetN`.

  This function, then, looks a DHCP named
  `HostNetworkInterfaceType-NAME` where NAME is the name of the host
  interface"

  [vbox interface-name]
  (try (.findDHCPServerByNetworkName
        vbox
        (str "HostInterfaceNetworking-" interface-name))
       (catch Exception e nil)))

(defn create-dhcp-server [vbox interface-name]
  (conditions/with-vbox-exception-translation
    {:E_INVALIDARG "Host network interface name already exists."}
    (.createDHCPServer
     vbox
     (str "HostInterfaceNetworking-" interface-name))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (use '[vmfest.virtualbox.virtualbox :as vbox])

  ;; find by name or UUID
  (def my-machine (vbox/find-machine
                   "http://localhost:18083"
                   ""
                   ""
                   "CentOS Minimal"))
  ;; -> #:vmfest.virtualbox.model.machine{
  ;;           :id "197c694b-fb56-43ed-88f5-f62769134442",
  ;;           :server #:vmfest.virtualbox.model.server{
  ;;                      :username "",
  ;;                      :password ""},
  ;;           :location nil}

  ;; obtain all the attributes of machine
  (use 'vmfest.virtualbox.model)
  (pprint (as-map my-machine))
  ;; -> {:id "197c694b-fb56-43ed-88f5-f62769134442",
  ;;     :server {:url "http://localhost:18083", :username "", :password ""},
  ;;     :location nil,
  ;;     :current-snapshot nil,
  ;;     :cpu-hot-plug-enabled? false,
  ;;     :settings-file-path
  ;;             "/User.../Machines/CentOS Minimal/CentOS Minimal.xml",
  ;;     :hpet-enabled false,
  ;;     :teleporter-port 0,
  ;;     :cpu-count 1,
  ;;     :snapshot-folder
  ;;             "/Users/tbatchelli/Library/VirtualBox/Machines/CentOS
  ;;             Minimal/Snapshots",
  ;; etc.... }

  ;; operate the machine
  (use '[vmfest.virtualbox.machine :as machine])
  (machine/start my-machine)
  (machine/pause my-machine)
  (machine/resume my-machine)
  (machine/stop my-machine)

  ;; query the virtualbox for objects
  (def server (vmfest.virtualbox.model.Server. "http://localhost:18083" "" ""))
  (vbox/machines server)

  ;; operate on machines
  (use '[vmfest.virtualbox.session :as session])

  ;; read-only
  (session/with-no-session my-machine [machine]
    (.getMemorySize machine))

  ;; read/write
  (session/with-direct-session my-machine [session machine]
    (.setMemorySize machine (long 1024))
    (.saveSettings machine)))
