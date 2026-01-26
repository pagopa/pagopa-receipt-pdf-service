#!/bin/bash
# Generated with `generate_imports.py`

# resource.azurerm_api_management_api_version_set.api_receipts_api
echo 'Importing azurerm_api_management_api_version_set.api_receipts_api'
sh terraform.sh import weu-uat 'azurerm_api_management_api_version_set.api_receipts_api' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/apiVersionSets/u-receipts-service-api'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_api.this'
sh terraform.sh import weu-uat 'module.apim_api_receipts_api_v1.azurerm_api_management_api.this' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/apis/pagopa-u-weu-receipts-receipts-service-api-v1;rev=1'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_api_policy.this[0]'
sh terraform.sh import weu-uat 'module.apim_api_receipts_api_v1.azurerm_api_management_api_policy.this[0]' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/apis/pagopa-u-weu-receipts-receipts-service-api-v1'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_product_api.this["receipts"]'
sh terraform.sh import weu-uat 'module.apim_api_receipts_api_v1.azurerm_api_management_product_api.this["receipts"]' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/products/receipts/apis/pagopa-u-weu-receipts-receipts-service-api-v1'


# module.apim_receipts_product
echo 'Importing module.apim_receipts_product.azurerm_api_management_product.this'
sh terraform.sh import weu-uat 'module.apim_receipts_product.azurerm_api_management_product.this' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/products/receipts'


# module.apim_receipts_product
echo 'Importing module.apim_receipts_product.azurerm_api_management_product_policy.this[0]'
sh terraform.sh import weu-uat 'module.apim_receipts_product.azurerm_api_management_product_policy.this[0]' '/subscriptions/26abc801-0d8f-4a6e-ac5f-8e81bcc09112/resourceGroups/pagopa-u-api-rg/providers/Microsoft.ApiManagement/service/pagopa-u-apim/products/receipts'


echo 'Import executed succesfully on uat environment! ⚡'
