/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.auth.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WelcomePage {

    private WebDriver driver;

    @FindBy(name = "username")
    private WebElement usernameInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(name = "passwordConfirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(css = "button[type=\"submit\"]")
    private WebElement createButton;

    @FindBy(css = "h1[text=\"Welcome to \"]")
    private WebElement welcomeMessage;

    @FindBy(css = "a[text=\"Administration Console \"]")
    private WebElement adminConsole;

    public WelcomePage(WebDriver driver) {
        this.driver = driver;

        AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
        PageFactory.initElements(ajax,this);
    }

    public void navigateTo(String url) {
        this.driver.get(url);
    }

    public boolean isPasswordSet() {
        return !(this.driver.getPageSource().contains("To get started with Keycloak, you first create an administrative user.") ||
                 this.driver.getPageSource().contains("You will need local access to create the administrative user."));
    }

    public void setPassword(String username, String password) {
        this.usernameInput.sendKeys(username);
        this.passwordInput.sendKeys(password);
        this.passwordConfirmationInput.sendKeys(password);

        this.createButton.click();

        if (!this.driver.getPageSource().contains("User created")) {
            throw new RuntimeException("Failed to updated password");
        }
    }

    public void navigateToAdminConsole() {
        this.adminConsole.click();
    }

    public String getWelcomeMessage() {
        return this.welcomeMessage.getText();
    }

}
