import { JSX } from 'react';
import styles from './Footer.module.css';
import appSettings from '../../shared/settings/settings';

function Footer(): JSX.Element {
  const currentYear = new Date().getFullYear();
  const appName = appSettings.appName;

  return (
    <footer className={styles.footer}>
      <div className={`container ${styles.footerContent}`}>
        <p>&copy; {currentYear} {appName}. All rights reserved.</p>
      </div>
    </footer>
  );
}

export default Footer;