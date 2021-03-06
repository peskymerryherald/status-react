(ns status-im.ui.screens.add-new.new-chat.events
  (:require [re-frame.core :as re-frame]
            [status-im.ui.screens.add-new.new-chat.db :as db]
            [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.ethereum.ens :as ens]
            [status-im.utils.ethereum.resolver :as resolver]
            [status-im.utils.handlers :as handlers]
            [clojure.string :as string]))

(re-frame/reg-fx
 :resolve-whisper-identity
 (fn [{:keys [web3 registry ens-name cb]}]
   (resolver/pubkey web3 registry ens-name cb)))

(handlers/register-handler-fx
 :new-chat/set-new-identity
 (fn [{{:keys [web3 network network-status] :as db} :db} [_ new-identity]]
   (let [new-identity-error (db/validate-pub-key db new-identity)]
     (if (and (string? new-identity)
              (string/starts-with? new-identity "0x"))
       {:db (assoc db
                   :contacts/new-identity       new-identity
                   :contacts/new-identity-error new-identity-error)}
       (let [network (get-in db [:account/account :networks network])
             chain   (ethereum/network->chain-keyword network)]
         {:resolve-whisper-identity {:web3 web3
                                     :registry (get ens/ens-registries chain)
                                     :ens-name (if (ens/is-valid-eth-name? new-identity)
                                                 new-identity
                                                 (str new-identity ".stateofus.eth"))
                                     :cb #(re-frame/dispatch [:new-chat/set-new-identity %])}})))))
