using System;
using System.Collections.Generic;

namespace CodeQuest.Api.Models;

public partial class Mission
{
    public int Id { get; set; }

    public string Titre { get; set; } = null!;

    public string Enonce { get; set; } = null!;

    public string Difficulte { get; set; } = null!;

    public int Xp { get; set; }

    public string CodeDepart { get; set; } = null!;

    public virtual ICollection<Soumission> Soumissions { get; set; } = new List<Soumission>();

    public virtual ICollection<Test> Tests { get; set; } = new List<Test>();
}
