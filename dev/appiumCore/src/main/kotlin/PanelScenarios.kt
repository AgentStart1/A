/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

suspend fun scenarioOpenAllUsersFromOverview(driver: AppTestDriver, expandedNavigation: Boolean) {
    driver.assertVisibleByDescription("Menu")
    if (!expandedNavigation) driver.clickByDescription("Menu")
    driver.clickByText("All users")
    driver.assertVisibleByText("All users")
    driver.assertNotVisibleByText("Sign in")
}
