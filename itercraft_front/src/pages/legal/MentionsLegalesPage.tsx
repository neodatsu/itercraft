import { Link } from 'react-router-dom';
import './legal.css';

export function MentionsLegalesPage() {
  return (
    <div className="legal-page">
      <h1>Mentions légales</h1>
      <p className="legal-intro">
        Conformément aux dispositions de la loi n° 2004-575 du 21 juin 2004 pour la confiance dans
        l'économie numérique (LCEN), les informations suivantes sont portées à la connaissance des
        utilisateurs du site itercraft.com.
      </p>

      <h2>Éditeur du site</h2>
      <p>
        Le site itercraft.com est édité par Itercraft.<br />
        Directeur de la publication : le responsable d'Itercraft.
      </p>

      <h2>Hébergement</h2>
      <p>
        Le site est hébergé par Amazon Web Services (AWS).<br />
        Adresse : Amazon Web Services EMEA SARL, 38 avenue John F. Kennedy, L-1855 Luxembourg.<br />
        Site web : <a href="https://aws.amazon.com" target="_blank" rel="noopener noreferrer">aws.amazon.com</a>
      </p>
      <p>
        Le nom de domaine est géré par Cloudflare, Inc.<br />
        Adresse : 101 Townsend St, San Francisco, CA 94107, États-Unis.<br />
        Site web : <a href="https://www.cloudflare.com" target="_blank" rel="noopener noreferrer">cloudflare.com</a>
      </p>

      <h2>Propriété intellectuelle</h2>
      <p>
        L'ensemble du contenu du site (textes, images, graphismes, logo, icônes, logiciels) est la
        propriété exclusive d'Itercraft ou de ses partenaires et est protégé par les lois françaises
        et internationales relatives à la propriété intellectuelle. Toute reproduction, représentation
        ou diffusion, en tout ou partie, du contenu de ce site est interdite sans autorisation
        préalable écrite d'Itercraft.
      </p>

      <h2>Données personnelles</h2>
      <p>
        Le traitement des données personnelles est décrit dans notre{' '}
        <Link to="/confidentialite">Politique de confidentialité</Link>.
      </p>

      <h2>Cookies</h2>
      <p>
        L'utilisation des cookies est décrite dans notre{' '}
        <Link to="/cookies">Politique de cookies</Link>.
      </p>

      <h2>Limitation de responsabilité</h2>
      <p>
        Itercraft s'efforce d'assurer l'exactitude et la mise à jour des informations diffusées sur
        ce site. Toutefois, Itercraft ne peut garantir l'exactitude, la précision ou l'exhaustivité
        des informations mises à disposition. En conséquence, Itercraft décline toute responsabilité
        pour les imprécisions, inexactitudes ou omissions portant sur des informations disponibles
        sur le site.
      </p>

      <h2>Droit applicable</h2>
      <p>
        Les présentes mentions légales sont régies par le droit français. En cas de litige, les
        tribunaux français seront seuls compétents.
      </p>
    </div>
  );
}
