using System;
using System.Collections.Generic;

namespace CodeQuest.Api.Models;

public partial class Soumission
{
    public int Id { get; set; }

    public int IdUtilisateur { get; set; }

    public int IdMission { get; set; }

    public string Code { get; set; } = null!;

    public string Verdict { get; set; } = null!;

    public int TempsExecution { get; set; }

    public DateTime Date { get; set; }

    public virtual Mission IdMissionNavigation { get; set; } = null!;

    public virtual Utilisateur IdUtilisateurNavigation { get; set; } = null!;
}
