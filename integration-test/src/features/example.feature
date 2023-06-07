Feature: feat

  Background:
    Given some

  Scenario: scen1
    Given initial json
    """
    {}
    """
    When the client send POST to /yourservice
    Then check statusCode is 201
    And check response body is
    """
    {}
    """
