package es.eriktorr
package accounts.domain

enum Rejection:
  case ExistingAccount
  case NoSuchAccount
  case InsufficientBalance
  case NotSettled
  case AlreadyClosed
  case BadRequest
