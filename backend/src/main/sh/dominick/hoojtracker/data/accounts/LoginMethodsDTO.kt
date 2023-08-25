package sh.dominick.hoojtracker.data.accounts

class LoginMethodsDTO(
    val password: Boolean
) {
    constructor(account: Account) : this(
        account.activeCredentials != null
    )
}