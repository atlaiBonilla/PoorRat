/**
 * PoorRat Security & Privacy Portal Client Scripts
 */

document.addEventListener('DOMContentLoaded', () => {
  // Mobile menu toggle
  const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
  const navLinks = document.querySelector('.nav-links');

  if (mobileMenuBtn && navLinks) {
    mobileMenuBtn.addEventListener('click', () => {
      navLinks.classList.toggle('open');
      const isExpanded = navLinks.classList.contains('open');
      mobileMenuBtn.setAttribute('aria-expanded', isExpanded);
    });
  }

  // Theme toggle
  const themeToggleBtn = document.getElementById('theme-toggle-btn');
  const savedTheme = localStorage.getItem('poorrat_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', newTheme);
      localStorage.setItem('poorrat_theme', newTheme);
      updateThemeIcon(newTheme);
    });
  }

  function updateThemeIcon(theme) {
    if (!themeToggleBtn) return;
    const iconSpan = themeToggleBtn.querySelector('.theme-icon');
    if (iconSpan) {
      iconSpan.textContent = theme === 'dark' ? '☀️' : '🌙';
    }
  }

  // Active link in Table of Contents on scroll
  const sections = document.querySelectorAll('.doc-section');
  const tocLinks = document.querySelectorAll('.toc-list a');

  if (sections.length > 0 && tocLinks.length > 0) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const id = entry.target.getAttribute('id');
            tocLinks.forEach((link) => {
              if (link.getAttribute('href') === `#${id}`) {
                link.classList.add('active');
              } else {
                link.classList.remove('active');
              }
            });
          }
        });
      },
      { rootMargin: '-20% 0px -70% 0px' }
    );

    sections.forEach((sec) => observer.observe(sec));
  }

  // Account Deletion Form Handler
  const deleteForm = document.getElementById('account-deletion-form');
  if (deleteForm) {
    deleteForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const emailInput = document.getElementById('delete-email');
      const reasonInput = document.getElementById('delete-reason');
      const confirmCheck = document.getElementById('delete-confirm-check');
      const feedbackDiv = document.getElementById('delete-feedback');

      if (!confirmCheck.checked) {
        alert('Por favor, marca la casilla de confirmación para proceder con la solicitud.');
        return;
      }

      const email = emailInput ? emailInput.value.trim() : '';
      const reason = reasonInput ? reasonInput.value.trim() : 'Solicitud directa del usuario';

      // Generate mailto link as fallback & display success feedback
      const subject = encodeURIComponent(`Solicitud de Eliminación de Cuenta PoorRat - ${email}`);
      const body = encodeURIComponent(
        `Hola Equipo de PoorRat,\n\nPor la presente solicito la eliminación total y definitiva de mi cuenta y de todos los datos asociados en la aplicación PoorRat (com.poorrat.gotchadev).\n\nCorreo registrado: ${email}\nMotivo: ${reason}\n\nConfirmo que deseo que se borren mis pesajes, rutinas, suplementos y registros alimentarios de forma irrevocable.\n\nFecha: ${new Date().toISOString()}`
      );

      const mailtoUrl = `mailto:support@gotchadev.com,privacy@poorrat.app?subject=${subject}&body=${body}`;

      if (feedbackDiv) {
        feedbackDiv.style.display = 'block';
        feedbackDiv.innerHTML = `
          <div class="callout success">
            <strong>✅ Solicitud Registrada Correctamente</strong>
            <p style="margin: 0.5rem 0 0.75rem;">Hemos procesado tu requerimiento para la cuenta: <code>${email}</code>. Tu solicitud será atendida y eliminada de nuestros servidores en un plazo máximo de 30 días naturales sin costo alguno.</p>
            <p style="margin: 0;">Para confirmación inmediata directa, puedes también <a href="${mailtoUrl}" style="color: inherit; text-decoration: underline; font-weight: bold;">enviar la notificación directa por correo electrónico</a>.</p>
          </div>
        `;
      }

      // Try triggering mail client if appropriate
      window.location.href = mailtoUrl;
    });
  }
});

