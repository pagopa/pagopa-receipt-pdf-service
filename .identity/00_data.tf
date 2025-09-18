data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_storage_account" "tfstate_app" {
  name                = "pagopainfraterraform${var.env}"
  resource_group_name = "io-infra-rg"
}
data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_user_assigned_identity" "identity_cd_01" {
  resource_group_name = "${local.product}-identity-rg"
  name                = "${local.product}-${local.domain}-job-01-github-cd-identity"
}

data "azurerm_user_assigned_identity" "workload_identity_clientid" {
  name                = "receipts-workload-identity"
  resource_group_name = "pagopa-${var.env_short}-${local.location_short}-${var.env}-aks-rg"
}

data "azurerm_key_vault" "key_vault" {
  name                = "pagopa-${var.env_short}-kv"
  resource_group_name = "pagopa-${var.env_short}-sec-rg"
}

data "azurerm_key_vault" "domain_key_vault" {
  name                = "pagopa-${var.env_short}-${local.domain}-kv"
  resource_group_name = "pagopa-${var.env_short}-${local.domain}-sec-rg"
}

data "azurerm_key_vault_secret" "key_vault_sonar" {
  name         = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_bot_cd_token" {
  name         = "pagopa-platform-domain-github-bot-cd-pat"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_cucumber_token" {
  name         = "cucumber-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_storage_account" "receipts_sa" {
  name                = "pagopa${var.env_short}${local.location_short}receiptsfnsa"
  resource_group_name = "pagopa-${var.env_short}-${local.location_short}-receipts-st-rg"
}

data "azurerm_cosmosdb_account" "receipts_cosmos" {
  name                = "pagopa-${var.env_short}-${local.location_short}-receipts-ds-cosmos-account"
  resource_group_name = "pagopa-${var.env_short}-${local.location_short}-receipts-rg"
}

data "azurerm_key_vault_secret" "key_vault_integration_test_subkey" {
  name         = "apikey-service-receipt" # "integration-test-subkey"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_tokenizer_api_key" {
  name         = "tokenizer-api-key"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_webhook_slack" {
  name         = "pagopa-pagamenti-integration-test-slack-webhook"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_deploy_webhook_slack" {
  name         = "pagopa-pagamenti-deploy-slack-webhook"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}