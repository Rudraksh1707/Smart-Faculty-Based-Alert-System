// Global App Logic
document.addEventListener('DOMContentLoaded', () => {
    // Check Authentication completely
    checkAuth();
    
    // Setup Sidebar Interactions
    setupSidebar();
    
    // Inject SweetAlert if not present (using basic CDN injection for UI alerts)
    if (typeof Swal === 'undefined') {
        const swalScript = document.createElement('script');
        swalScript.src = "https://cdn.jsdelivr.net/npm/sweetalert2@11";
        document.head.appendChild(swalScript);
    }
});

function checkAuth() {
    // Exclude login page from auth check
    if (window.location.pathname.endsWith('index.html') || window.location.pathname.endsWith('/frontend/')) {
        if (localStorage.getItem('adminUser')) {
            window.location.href = 'dashboard.html';
        }
        return;
    }

    // For all other pages
    const adminUser = localStorage.getItem('adminUser');
    if (!adminUser) {
        window.location.href = 'index.html';
    } else {
        // Initialize user display
        const userNameDisplays = document.querySelectorAll('.user-name-display');
        const user = JSON.parse(adminUser);
        userNameDisplays.forEach(el => el.textContent = user.username);
    }
}

function setupSidebar() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            Swal.fire({
                title: 'Are you sure?',
                text: "You will be logged out of your session",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#4F46E5',
                cancelButtonColor: '#EF4444',
                confirmButtonText: 'Yes, log out!'
            }).then((result) => {
                if (result.isConfirmed) {
                    localStorage.removeItem('adminUser');
                    window.location.href = 'index.html';
                }
            })
        });
    }

    // Highlight active nav
    const paths = window.location.pathname.split('/');
    const currentFile = paths[paths.length - 1];
    document.querySelectorAll('.nav-item').forEach(link => {
        if (link.getAttribute('href') === currentFile) {
            link.classList.add('active');
        }
    });
}

// Modal helper functions
function showModal(modalId) {
    document.getElementById(modalId).classList.add('active');
}

function hideModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// Attach close event to all modal close buttons
document.querySelectorAll('.close-modal').forEach(btn => {
    btn.addEventListener('click', (e) => {
        const modalId = e.target.closest('.modal-overlay').id;
        hideModal(modalId);
    });
});
