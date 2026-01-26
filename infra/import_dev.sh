#!/bin/bash
# Generated with `generate_imports.py`

# resource.azurerm_api_management_api_version_set.api_receipts_api
echo 'Importing azurerm_api_management_api_version_set.api_receipts_api'
sh terraform.sh import weu-dev 'azurerm_api_management_api_version_set.api_receipts_api' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/apiVersionSets/d-receipts-service-api'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_api.this'
sh terraform.sh import weu-dev 'module.apim_api_receipts_api_v1.azurerm_api_management_api.this' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/apis/pagopa-d-weu-receipts-receipts-service-api-v1;rev=1'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_api_policy.this[0]'
sh terraform.sh import weu-dev 'module.apim_api_receipts_api_v1.azurerm_api_management_api_policy.this[0]' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/apis/pagopa-d-weu-receipts-receipts-service-api-v1'


# module.apim_api_receipts_api_v1
echo 'Importing module.apim_api_receipts_api_v1.azurerm_api_management_product_api.this["receipts"]'
sh terraform.sh import weu-dev 'module.apim_api_receipts_api_v1.azurerm_api_management_product_api.this["receipts"]' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/products/receipts/apis/pagopa-d-weu-receipts-receipts-service-api-v1'


# module.apim_receipts_product
echo 'Importing module.apim_receipts_product.azurerm_api_management_product.this'
sh terraform.sh import weu-dev 'module.apim_receipts_product.azurerm_api_management_product.this' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/products/receipts'


# module.apim_receipts_product
echo 'Importing module.apim_receipts_product.azurerm_api_management_product_policy.this[0]'
sh terraform.sh import weu-dev 'module.apim_receipts_product.azurerm_api_management_product_policy.this[0]' '/subscriptions/bbe47ad4-08b3-4925-94c5-1278e5819b86/resourceGroups/pagopa-d-api-rg/providers/Microsoft.ApiManagement/service/pagopa-d-apim/products/receipts'


echo 'Import executed succesfully on dev environment! ⚡'
