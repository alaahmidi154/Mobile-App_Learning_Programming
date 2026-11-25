using System;
using System.Collections.Generic;

namespace CodeQuest.Api.Models;

public partial class Utilisateur
{
    public int Id { get; set; }

    public string Nom { get; set; } = null!;

    public string Email { get; set; } = null!;

    public string MotDePasseHache { get; set; } = null!;

    public int XpTotal { get; set; }

    public int NiveauJoueur { get; set; }

    public string Role { get; set; } = null!;

    public virtual ICollection<Soumission> Soumissions { get; set; } = new List<Soumission>();
}
