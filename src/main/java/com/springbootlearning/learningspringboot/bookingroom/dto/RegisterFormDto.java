package com.springbootlearning.learningspringboot.bookingroom.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public class RegisterFormDto {

    @NotBlank(message = "Le prenom est obligatoire")
    @Length(min = 2, max = 50, message = "Le prenom doit contenir entre 2 et 50 caracteres")
    private String firstname;

    @NotBlank(message = "Le nom est obligatoire")
    @Length(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caracteres")
    private String lastname;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Length(min = 3, max = 30, message = "Le nom d'utilisateur doit contenir entre 3 et 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit etre valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Length(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+",
             message = "Le mot de passe doit contenir au moins une minuscule, une majuscule, un chiffre et un caractere special")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    // La contrainte s'applique a la propriete "passwordConfirmed" : le template
    // affiche l'erreur sous le champ de confirmation.
    // Si un des deux champs est vide, on laisse @NotBlank produire le message
    // adapte plutot que d'ajouter une fausse erreur de non-correspondance.
    @AssertTrue(message = "Les mots de passe ne correspondent pas")
    public boolean isPasswordConfirmed() {
        if (password == null || password.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
