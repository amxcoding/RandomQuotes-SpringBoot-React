import { JSX } from 'react';
import { NavLink, Link } from 'react-router-dom';
import styles from './Header.module.css';


function Header(): JSX.Element {

  // Helper function for applying active styles to NavLink
  const getNavLinkClass = ({ isActive }: { isActive: boolean }): string => {
      return isActive ? `${styles.navLink} ${styles.active}` : styles.navLink;
  };

  return (
      <header className={styles.header}>
          <div className={`container ${styles.headerContent}`}>
              <Link to="/" className={styles.logo}>
                  <span>RandomQuotes</span>
              </Link>

              <nav className={styles.navigation}>
                  <ul>
                      <li>
                          {/* NavLink automatically handles active state */}
                          {/* 'end' ensures exact match for home */}
                          <NavLink to="/" className={getNavLinkClass} end> 
                              Home
                          </NavLink>
                      </li>
                      <li>
                          <NavLink to="/about" className={getNavLinkClass}>
                              About
                          </NavLink>
                      </li>
                  </ul>
              </nav>
          </div>
      </header>
  );
}

export default Header;