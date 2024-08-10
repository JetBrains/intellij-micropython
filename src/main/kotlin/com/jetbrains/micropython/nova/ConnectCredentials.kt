package com.jetbrains.micropython.nova

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.annotations.Nls
import java.net.URI
import java.net.URISyntaxException


private val PASSWORD_LENGHT = 4..9
@Service(Service.Level.PROJECT)
class ConnectCredentials {

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName("MySystem", key)
        )
    }

    private fun key(url: String) = "${this::class.java.name}/$url"

    fun retrieveUrlAndPassword(): Pair<String, String> {
        val url = PropertiesComponent.getInstance().getValue(this::class.java.name) ?: "ws://<host>:8266"

        val attributes = createCredentialAttributes(key(url))
        val passwordSafe = PasswordSafe.instance
        val password = passwordSafe.getPassword(attributes) ?: ""

        return url to password
    }

    fun saveUrlPassword(url: String, password: String) {
        PropertiesComponent.getInstance().setValue(this::class.java.name, url)
        val attributes = createCredentialAttributes(key(url))
        val credentials = Credentials(null, password)
        PasswordSafe.instance.set(attributes, credentials)
    }
}

fun uriOrMessageUrl(url: String): Pair<URI?,@Nls String?> {
    try {
        val uri = URI(url)
        if (uri.scheme !in arrayOf("ws", "wss")) {
            return null to "URL format is ws://host:port or wss://host:port"
        }
        return uri to null
    } catch (_: URISyntaxException) {
        return null to "Malformed URL"
    }
}

internal fun askCredentials(project: Project): Boolean {
    var (url, password) = project.service<ConnectCredentials>().retrieveUrlAndPassword()
    val confirmation =
        with(Disposer.newDisposable()) {
            val panel = panel {
                row {
                    textField().bindText({ url }, { url = it })
                        .label("URL: ").columns(40)
                        .validationInfo { field ->
                            val (_, msg) = uriOrMessageUrl(field.text)
                            msg?.let { error(it).withOKEnabled() }
                        }

                }.layout(RowLayout.LABEL_ALIGNED)
                row {
                    passwordField()
                        .bindText({ password }, { password = it })
                        .label("Password (4..9 symbols): ").columns(40)
                        .validationInfo { field ->
                            if (field.password.size !in PASSWORD_LENGHT) error("Allowed password length is $PASSWORD_LENGHT").withOKEnabled() else null
                        }
                }.layout(RowLayout.LABEL_ALIGNED)

            }.apply {
                registerValidators(this@with)
                validateAll()
            }
            DialogBuilder(project).centerPanel(panel).apply {
                addOkAction()
                addCancelAction()
            }.show()
        }
    if (confirmation == OK_EXIT_CODE) {
        project.service<ConnectCredentials>().saveUrlPassword(url, password)
        return true
    } else {
        return false
    }
}

class ConnectionParameters : DumbAwareAction("Connection", null, AllIcons.General.User) {
    //todo move to setup page?
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.apply { askCredentials(this) }
    }

}