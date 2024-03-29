resource "azurerm_role_assignment" "main_terraform_subscription" {
  scope                = data.azurerm_subscription.current.id
  role_definition_name = "Reader"
  principal_id         = azuread_service_principal.main.object_id
}

resource "azurerm_role_assignment" "main_terraform_storage_account_tfstate_app" {
  scope                = data.azurerm_storage_account.tfstate_app.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.main.object_id
}
resource "azurerm_role_assignment" "main_terraform_storage_account_tfstate_app_github_aks" {
  scope                = data.azurerm_storage_account.tfstate_app.id
  role_definition_name = "Contributor"
  principal_id         = module.github_runner_app.object_id
}

resource "azurerm_role_assignment" "main_terraform_resource_group_dashboards" {
  scope                = data.azurerm_resource_group.dashboards.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.main.object_id
}