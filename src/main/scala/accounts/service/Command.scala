package es.eriktorr
package accounts.service

enum Command:
  case Open
  case Deposit(amount: BigDecimal)
  case Withdraw(amount: BigDecimal)
  case Close
