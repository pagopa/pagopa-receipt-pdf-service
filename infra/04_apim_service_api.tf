##############
## Products ##
##############

module "apim_receipts_product" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_product?ref=v8.103.0"

  product_id   = "receipts"
  display_name = "Receipts Service PDF"
  description  = "Servizio per gestire recupero ricevute"

  resource_group_name = local.apim.rg
  api_management_name = local.apim.name

  published             = true
  subscription_required = true
  approval_required     = true
  subscriptions_limit   = 1000

  policy_xml = file("./api_product/receipt-service/_base_policy.xml")
}

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
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v8.103.0"

  name                  = format("%s-receipts-service-api", local.project)
  api_management_name = local.apim.name
  resource_group_name = local.apim.rg
  product_ids           = [module.apim_receipts_product.product_id]
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

  xml_content = templatefile("./api/receipt-service/v1/_base_policy.xml", {
    hostname = local.receipts_hostname
  })
}