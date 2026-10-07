document.addEventListener('DOMContentLoaded', () => {
    const shortenBtn = document.getElementById('shortenBtn');
    const longUrlInput = document.getElementById('longUrlInput');
    const resultContainer = document.getElementById('resultContainer');
    const shortUrlLink = document.getElementById('shortUrlLink');
    const copyBtn = document.getElementById('copyBtn');
    const notificationArea = document.getElementById('notificationArea');

    function showNotification(message, isError = false) {
        notificationArea.textContent = message;
        notificationArea.className = isError ? 'error' : '';
        notificationArea.classList.remove('hidden');
    }

    shortenBtn.addEventListener('click', async () => {
        const url = longUrlInput.value.trim();

        if (!url) {
            showNotification('Please enter a valid URL.', true);
            return;
        }

        // Basic frontend URL validation
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            showNotification('URL must start with http:// or https://', true);
            return;
        }

        notificationArea.classList.add('hidden');
        shortenBtn.disabled = true;
        shortenBtn.textContent = 'Working...';

        try {
            // Send the POST request to your Javalin backend
            const response = await fetch('http://localhost:8080/shorten', {                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain'
                },
                body: url
            });

            if (response.ok) {
                const shortUrl = await response.text();
                shortUrlLink.href = shortUrl;
                shortUrlLink.textContent = shortUrl;
                resultContainer.classList.remove('hidden');
                longUrlInput.value = ''; // Clear input
            } else {
                const errorText = await response.text();
                showNotification(`Error: ${errorText}`, true);
            }
        } catch (error) {
            showNotification('Server connection failed. Is your Java backend running?', true);
        } finally {
            shortenBtn.disabled = false;
            shortenBtn.textContent = 'Shorten';
        }
    });

    copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(shortUrlLink.textContent).then(() => {
            const originalText = copyBtn.textContent;
            copyBtn.textContent = 'Copied!';
            copyBtn.style.backgroundColor = 'var(--success-color)';

            setTimeout(() => {
                copyBtn.textContent = originalText;
                copyBtn.style.backgroundColor = '#334155';
            }, 2000);
        });
    });
});