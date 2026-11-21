using System;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;

namespace CodeQuest.Api.Models;

public partial class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options)
        : base(options)
    {
    }

    public virtual DbSet<Mission> Missions { get; set; }

    public virtual DbSet<Soumission> Soumissions { get; set; }

    public virtual DbSet<Test> Tests { get; set; }

    public virtual DbSet<Utilisateur> Utilisateurs { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Mission>(entity =>
        {
            entity.HasKey(e => e.Id).HasName("Mission_pkey");

            entity.ToTable("Mission");

            entity.Property(e => e.Difficulte).HasMaxLength(20);
            entity.Property(e => e.Titre).HasMaxLength(150);
        });

        modelBuilder.Entity<Soumission>(entity =>
        {
            entity.HasKey(e => e.Id).HasName("Soumission_pkey");

            entity.ToTable("Soumission");

            entity.HasIndex(e => new { e.IdUtilisateur, e.IdMission, e.Verdict }, "IX_Soumission_User");

            entity.Property(e => e.Date).HasDefaultValueSql("now()");
            entity.Property(e => e.Verdict).HasMaxLength(20);

            entity.HasOne(d => d.IdMissionNavigation).WithMany(p => p.Soumissions)
                .HasForeignKey(d => d.IdMission)
                .OnDelete(DeleteBehavior.ClientSetNull)
                .HasConstraintName("Soumission_IdMission_fkey");

            entity.HasOne(d => d.IdUtilisateurNavigation).WithMany(p => p.Soumissions)
                .HasForeignKey(d => d.IdUtilisateur)
                .OnDelete(DeleteBehavior.ClientSetNull)
                .HasConstraintName("Soumission_IdUtilisateur_fkey");
        });

        modelBuilder.Entity<Test>(entity =>
        {
            entity.HasKey(e => e.Id).HasName("Test_pkey");

            entity.ToTable("Test");

            entity.HasIndex(e => e.IdMission, "IX_Test_Mission");

            entity.Property(e => e.Visible).HasDefaultValue(true);

            entity.HasOne(d => d.IdMissionNavigation).WithMany(p => p.Tests)
                .HasForeignKey(d => d.IdMission)
                .HasConstraintName("Test_IdMission_fkey");
        });

        modelBuilder.Entity<Utilisateur>(entity =>
        {
            entity.HasKey(e => e.Id).HasName("Utilisateur_pkey");

            entity.ToTable("Utilisateur");

            entity.HasIndex(e => e.XpTotal, "IX_Utilisateur_Xp").IsDescending();

            entity.HasIndex(e => e.Email, "Utilisateur_Email_key").IsUnique();

            entity.Property(e => e.Email).HasMaxLength(200);
            entity.Property(e => e.MotDePasseHache).HasMaxLength(200);
            entity.Property(e => e.NiveauJoueur).HasDefaultValue(1);
            entity.Property(e => e.Nom).HasMaxLength(100);
            entity.Property(e => e.Role)
                .HasMaxLength(20)
                .HasDefaultValueSql("'joueur'::character varying");
        });

        OnModelCreatingPartial(modelBuilder);
    }

    partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
}
