import { JSX } from 'react';
import { Outlet } from 'react-router-dom';
import Footer from '../Footer/Footer';
import Header from '../Header/Header';
import styles from './Layout.module.css';

function Layout(): JSX.Element {
  return (
    <div className={styles.appContainer}>
      <Header />
      <main className={`container ${styles.content}`}>
        {/* Routed page content will render here */}
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

export default Layout;
