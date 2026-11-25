using System;
using System.Collections.Generic;

namespace CodeQuest.Api.Models;

public partial class Test
{
    public int Id { get; set; }

    public int IdMission { get; set; }

    public string Entree { get; set; } = null!;

    public string SortieAttendue { get; set; } = null!;

    public bool Visible { get; set; }

    public virtual Mission IdMissionNavigation { get; set; } = null!;
}
