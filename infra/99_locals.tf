locals {
  product = "${var.prefix}-${var.env_short}"
  project = "${var.prefix}-${var.env_short}-${var.location_short}-${var.domain}"

  apim = {
    name       = "${local.product}-apim"
    rg         = "${local.product}-api-rg"
    helpdesk_api_product_id = "technical_support_api"
  }

  receipt_pdf_service_hostname = var.env == "prod" ? "weu${var.env}.receipt-pdf-service.internal.platform.pagopa.it" : "weu${var.env}.receipt-pdf-service.internal.${var.env}.platform.pagopa.it"
  receipt_pdf_service_url      = "https://${local.receipt_pdf_service_hostname}/pagopa-receipt-pdf-service"

  helpdesk_api = {
    display_name          = "Receipt PDF Service - Helpdesk API"
    description           = "Receipt PDF Service API for helpdesk support"
    published             = true
    subscription_required = true
    approval_required     = false
    subscriptions_limit   = 1000
    service_url           = local.receipt_pdf_service_url
    path                  = "receipts/helpdesk/service"
  }
}
