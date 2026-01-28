####################
# SERVICE API    #
####################

resource "azurerm_api_management_api_version_set" "api_receipts_api" {

  name                = format("%s-receipts-service-api", var.env_short)
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.receipts_service_api.display_name
  versioning_scheme   = "Segment"
}


module "apim_api_receipts_api_v1" {
  source = "./.terraform/modules/__v3__/api_management_api"

  name                  = format("%s-receipts-service-api", local.project)
  api_management_name = local.apim.name
  resource_group_name = local.apim.rg
  product_ids           = [local.apim.receipts_product_id]
  subscription_required = local.receipts_service_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_receipts_api.id
  api_version           = "v1"

  description  = local.receipts_service_api.description
  display_name = local.receipts_service_api.display_name
  path         = local.receipts_service_api.path
  protocols    = ["https"]
  service_url  = local.receipts_service_api.service_url

  content_format = "openapi"
  content_value = templatefile("../openapi/openapi.json", {
    host = local.apim.hostname
  })

  xml_content = file("./api/receipt-service/v1/_base_policy.xml")
}