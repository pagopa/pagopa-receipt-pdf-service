data "azurerm_key_vault" "kv" {
  name                = "${local.product}-${var.domain}-kv"
  resource_group_name = "${local.product}-${var.domain}-sec-rg"
}

resource "azurerm_key_vault_secret" "receipt_service_helpdesk_subkey" {
  count        = var.env_short != "p" ? 1 : 0
  name         = "receipt-service-helpdesk-integration-test-subkey"
  value        = azurerm_api_management_subscription.receipt_service_helpdesk_subkey[0].primary_key
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.kv.id
}

resource "azurerm_api_management_subscription" "receipt_service_helpdesk_subkey" {
  count = var.env_short != "p" ? 1 : 0

  api_management_name = local.apim.name
  resource_group_name = local.apim.rg
  api_id              = replace(module.apim_api_helpdesk_api_v1.id, ";rev=1", "")
  display_name        = "Subscription for Receipt Service Helpdesk integration tests"
  allow_tracing       = false
  state               = "active"
}

resource "azurerm_key_vault_secret" "receipt_service_internal_subkey" {
  count        = var.env_short != "p" ? 1 : 0
  name         = "apikey-service-internal-receipt"
  value        = azurerm_api_management_subscription.receipt_service_internal_subkey[0].primary_key
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.kv.id
}

resource "azurerm_api_management_subscription" "receipt_service_internal_subkey" {
  count = var.env_short != "p" ? 1 : 0

  api_management_name = local.apim.name
  resource_group_name = local.apim.rg
  api_id              = replace(module.apim_api_pdf_api_v1.id, ";rev=1", "")
  display_name        = "Subscription for Receipt Service Internal API"
  allow_tracing       = false
  state               = "active"
}