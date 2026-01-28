locals {
  product = "${var.prefix}-${var.env_short}"
  project = "${var.prefix}-${var.env_short}-${var.location_short}-${var.domain}"

  apim = {
    name       = "${local.product}-apim"
    rg         = "${local.product}-api-rg"
    hostname     = "api.${var.apim_dns_zone_prefix}.${var.apim_dns_zone_prefix}"
    helpdesk_api_product_id = "technical_support_api"
    receipts_product_id = "receipts"
  }

  receipts_hostname = var.env == "prod" ? "weu${var.env}.receipts.internal.platform.pagopa.it" : "weu${var.env}.receipts.internal.${var.env}.platform.pagopa.it"
  receipt_pdf_service_url = "https://${local.receipts_hostname}/pagopa-receipt-pdf-service"
  receipt_pdf_service_helpdesk_url = "https://${local.receipts_hostname}/pagopa-receipt-pdf-service-helpdesk"

  receipts_service_api = {
    display_name          = "Receipts Service PDF"
    description           = "API to handle receipts"
    path                  = "receipts/service"
    subscription_required = true
    service_url           = local.receipt_pdf_service_url
  }

  receipt_service_helpdesk_api = {
    display_name          = "Receipt PDF Service - Helpdesk API"
    description           = "Receipt PDF Service API for helpdesk support"
    published             = true
    subscription_required = true
    approval_required     = false
    subscriptions_limit   = 1000
    service_url           = local.receipt_pdf_service_helpdesk_url
    path                  = "receipts/helpdesk/service"
  }
}
