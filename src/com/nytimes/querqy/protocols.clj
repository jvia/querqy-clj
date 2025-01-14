(ns com.nytimes.querqy.protocols
  (:require
   [com.nytimes.querqy.context :as context])
  (:import
   (querqy.model ExpandedQuery Query)
   (querqy.rewrite ContextAwareQueryRewriter RewriteChain RewriterFactory SearchEngineRequestAdapter)))

(defprotocol Parser
  (parse ^Query [this ^String string]
    "Parse a string into in a `query.model.Query`"))

;; ----------------------------------------------------------------------

(defprotocol Rewriter
  "A rewriter transforms "
  (rewrite
    [this ^ExpandedQuery query]
    [this ^ExpandedQuery query ^SearchEngineRequestAdapter context]
    "Transform an ExpandedQuery into a new, potentially rewritten ExpandedQuery."))

;; Some default Rewriter implementations for the Querqy classes.

(extend-protocol Rewriter
  ContextAwareQueryRewriter
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (.rewrite this query context)))

  RewriteChain
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (.rewrite this query context)))

  RewriterFactory
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (let [rewriter (.createRewriter this query context)]
       (rewrite rewriter query context)))))

;; ----------------------------------------------------------------------

(defprotocol Emitter
  (emit [this query opts]
    "Emit a system-specific query for a given `querqy.model.ExpandedQuery`"))
