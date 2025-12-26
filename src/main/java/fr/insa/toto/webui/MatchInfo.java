/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.webui;

public class MatchInfo {
    public int idMatch;
    public int idTerrain;
    public String statut; // "EN_COURS" ou "CLOSE"
    
    public int idEquipe1;
    public String nomEquipe1 = "Equipe 1";
    public Integer score1; // Peut être null
    
    public int idEquipe2;
    public String nomEquipe2 = "Equipe 2";
    public Integer score2; // Peut être null

    public String getDescriptionDuel() {
        return nomEquipe1 + "  VS  " + nomEquipe2;
    }
}